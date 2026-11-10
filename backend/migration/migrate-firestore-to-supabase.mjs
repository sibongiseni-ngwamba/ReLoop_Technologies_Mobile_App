import { randomBytes } from "node:crypto";
import { readFile } from "node:fs/promises";
import { cert, initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { getFirestore } from "firebase-admin/firestore";
import { createClient } from "@supabase/supabase-js";

const required = ["FIREBASE_SERVICE_ACCOUNT_PATH", "SUPABASE_URL", "SUPABASE_SECRET_KEY"];
for (const name of required) {
  if (!process.env[name]) throw new Error(`Missing environment variable ${name}`);
}

const serviceAccount = JSON.parse(await readFile(process.env.FIREBASE_SERVICE_ACCOUNT_PATH, "utf8"));
const firebaseApp = initializeApp({ credential: cert(serviceAccount) }, "reloop-migration");
const sourceAuth = getAuth(firebaseApp);
const firestore = getFirestore(firebaseApp);
const supabase = createClient(process.env.SUPABASE_URL, process.env.SUPABASE_SECRET_KEY, {
  auth: { autoRefreshToken: false, persistSession: false },
});

const userIds = new Map();
const emailsToSupabaseUsers = new Map();

async function getSupabaseUserByEmail(email) {
  let page = 1;
  while (true) {
    const { data, error } = await supabase.auth.admin.listUsers({ page, perPage: 1000 });
    if (error) throw error;
    const found = data.users.find((user) => user.email?.toLowerCase() === email.toLowerCase());
    if (found) return found;
    if (data.users.length < 1000) return null;
    page += 1;
  }
}

async function createDestinationUser(email, metadata = {}) {
  const cached = emailsToSupabaseUsers.get(email.toLowerCase());
  if (cached) return cached;
  const existing = await getSupabaseUserByEmail(email);
  if (existing) {
    emailsToSupabaseUsers.set(email.toLowerCase(), existing);
    return existing;
  }
  const { data, error } = await supabase.auth.admin.createUser({
    email,
    email_confirm: true,
    password: randomBytes(32).toString("base64url"),
    user_metadata: metadata,
  });
  if (error) throw error;
  emailsToSupabaseUsers.set(email.toLowerCase(), data.user);
  return data.user;
}

async function importAuthUsers() {
  let pageToken;
  do {
    const page = await sourceAuth.listUsers(1000, pageToken);
    for (const sourceUser of page.users) {
      if (!sourceUser.email) continue;
      const destination = await createDestinationUser(sourceUser.email, {
        first_name: sourceUser.displayName?.trim().split(/\s+/)[0] ?? "",
        migrated_from_firebase_uid: sourceUser.uid,
      });
      userIds.set(sourceUser.uid, destination.id);
      emailsToSupabaseUsers.set(sourceUser.email.toLowerCase(), destination);
    }
    pageToken = page.pageToken;
  } while (pageToken);
}

function toIso(value) {
  if (value && typeof value.toDate === "function") return value.toDate().toISOString();
  if (Array.isArray(value)) return value.map(toIso);
  if (value && typeof value === "object") {
    return Object.fromEntries(Object.entries(value).map(([key, item]) => [key, toIso(item)]));
  }
  if (typeof value === "number" && value > 100000000000) return new Date(value).toISOString();
  return value;
}

function snake(value) {
  if (Array.isArray(value)) return value.map(snake);
  if (value && typeof value === "object") {
    return Object.fromEntries(Object.entries(value).map(([key, item]) => [
      key.replace(/[A-Z]/g, (letter) => `_${letter.toLowerCase()}`),
      snake(item),
    ]));
  }
  return value;
}

const collections = [
  ["users", "profiles", "id"],
  ["agents", "agents", "agent_id"],
  ["pickups", "pickups", "pickup_id"],
  ["rewards", "reward_accounts", "user_id"],
  ["rewardItems", "reward_items", "reward_item_id"],
  ["wasteCategories", "waste_categories", "waste_category_id"],
  ["guidanceContent", "guidance_content", "guidance_id"],
  ["wasteLogs", "waste_logs", "waste_log_id"],
  ["redemptionLogs", "redemption_logs", "redemption_id"],
  ["pointsTransactions", "points_transactions", "transaction_id"],
  ["notifications", "notifications", "notification_id"],
];

async function ensureProfileUsers(profileDocs) {
  for (const doc of profileDocs) {
    const profile = doc.data();
    const email = profile.email?.trim();
    if (!email) continue;
    const destination = await createDestinationUser(email, {
      first_name: profile.firstName ?? "",
      last_name: profile.lastName ?? "",
      migrated_from_firebase_uid: doc.id,
    });
    userIds.set(doc.id, destination.id);
    emailsToSupabaseUsers.set(email.toLowerCase(), destination);
  }
}

function transformDocument(collectionName, document) {
  const sourceId = document.id;
  const sourceRow = toIso(document.data());
  if (collectionName === "rewards") sourceRow.userId = sourceRow.userId ?? sourceId;
  if (sourceRow.userId) {
    const mapped = userIds.get(sourceRow.userId);
    if (!mapped) throw new Error(`No imported Supabase Auth user for Firebase UID ${sourceRow.userId} (${collectionName}/${sourceId})`);
    sourceRow.userId = mapped;
  }
  if (collectionName === "users") {
    const mappedId = userIds.get(sourceId);
    if (!mappedId) return null;
    sourceRow.id = mappedId;
    delete sourceRow.userId;
    if (sourceRow.role === "agent") sourceRow.role = "collector";
    if (sourceRow.role === "administrator") sourceRow.role = "admin";
  } else {
    const idKey = {
      agents: "agentId", pickups: "pickupId", rewardItems: "rewardItemId",
      wasteCategories: "wasteCategoryId", guidanceContent: "guidanceId",
      wasteLogs: "wasteLogId", redemptionLogs: "redemptionId",
      pointsTransactions: "transactionId", notifications: "notificationId",
    }[collectionName];
    if (idKey) sourceRow[idKey] = sourceRow[idKey] ?? sourceId;
  }
  return snake(sourceRow);
}

async function importCollection(sourceName, tableName, conflictColumn) {
  const snapshot = await firestore.collection(sourceName).get();
  const rows = snapshot.docs.map((doc) => transformDocument(sourceName, doc)).filter(Boolean);
  if (rows.length === 0) {
    console.log(`${sourceName}: 0 documents`);
    return;
  }
  for (let offset = 0; offset < rows.length; offset += 250) {
    const chunk = rows.slice(offset, offset + 250);
    const { error } = await supabase.from(tableName).upsert(chunk, { onConflict: conflictColumn });
    if (error) throw new Error(`Import failed for ${tableName}: ${error.message}`);
  }
  console.log(`${sourceName}: imported ${rows.length} documents into ${tableName}`);
}

const sourceProfiles = await firestore.collection("users").get();
await importAuthUsers();
await ensureProfileUsers(sourceProfiles.docs);
for (const [source, destination, key] of collections) {
  await importCollection(source, destination, key);
}

console.log(`Finished. Mapped ${userIds.size} Firebase user IDs to Supabase Auth IDs.`);
console.log("Firebase data was read only. Verify counts and relationships before retiring the Firebase project.");
