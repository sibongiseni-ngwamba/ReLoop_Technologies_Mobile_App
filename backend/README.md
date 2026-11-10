# ReLoop Backend

The Android source and backend are now wired to Supabase. The architecture follows the separation used by SideHustle: the Android app calls a REST API, and server-side code owns database operations. ReLoop's REST API is a Supabase Edge Function; PostgreSQL migrations and the function are deployed from GitHub Actions. Supabase project setup, production deployment, live Firebase data import, and live-app verification still require your project credentials and account access.

```text
Android app ── HTTPS + Supabase Auth access token ──> reloop-api Edge Function ──> Supabase PostgreSQL
```

The Android `RestApiClient` is the app's only business-data gateway. It must call the function for profiles, pickups, rewards, waste logs, guidance, and notifications. Do not place a Supabase service-role key in the Android app. The function uses the caller's access token and the public anon/publishable key, so PostgreSQL Row Level Security remains in force.

## Repository layout

```text
supabase/
  config.toml
  migrations/              versioned PostgreSQL schema and security policies
  functions/reloop-api/    authenticated REST endpoints
.github/workflows/
  deploy-supabase.yml      applies migrations and deploys the Edge Function on main
```

## Data model

The initial migration creates profiles, agents, pickups, reward accounts and catalogue, redemption and points ledgers, waste categories and logs, guidance, and notifications. User-owned records reference the Supabase Auth user UUID. Existing Firebase document identifiers are retained as text IDs for business records so Firestore document IDs can be migrated without changing references.

Row Level Security is enabled on every public table. The migration includes owner policies, staff policies, a profile/reward-account creation trigger, and transactional RPCs for awarding and redeeming points. Never disable RLS to make a client request work; fix the policy or endpoint instead.

## REST routes

All routes are under `/functions/v1/reloop-api`. Except `/health`, requests require `Authorization: Bearer <Supabase access token>`.

| Method | Route | Purpose |
|---|---|---|
| GET | `/health` | API health check |
| GET, PUT | `/users/{userId}` | Read/update the signed-in user's profile |
| GET, POST | `/pickups` | List visible pickups/create a pickup |
| GET, PATCH | `/pickups/{pickupId}` | Read/update a pickup |
| GET | `/rewards/account` | Read the signed-in user's balance |
| GET | `/rewards/items` | Read active rewards |
| GET, POST | `/rewards/transactions` | Read ledger / award points idempotently |
| POST | `/rewards/redemptions` | Redeem a catalogue item transactionally |
| GET, POST | `/waste-logs` | Read/create scan logs |
| GET | `/waste-categories` | Read active waste categories |
| GET, POST, PATCH, DELETE | `/notifications` | List/create/update/delete own notifications |
| GET | `/guidance` | Read active recycling guidance |

JSON request and response fields use the Kotlin model's camelCase names. Database columns use snake_case. Time values are milliseconds since epoch at the app boundary and `timestamptz` in PostgreSQL.

## Local setup

1. Install the Supabase CLI and Docker Desktop.
2. From the repository root, run `supabase start` to start the local Supabase stack and apply migrations.
3. For Android sign-in and API calls, configure a Supabase development project in local `local.properties` (see below); the hosted development project avoids emulator networking setup.
4. Use the local Supabase dashboard to inspect migrations or test the Edge Function with a local access token.
5. Stop the local stack with `supabase stop`.

Add these local-only values to `local.properties` for Android Studio. Do not commit this file:

```properties
SUPABASE_URL=https://YOUR_PROJECT_REF.supabase.co
SUPABASE_ANON_KEY=YOUR_PUBLISHABLE_OR_ANON_KEY
GOOGLE_WEB_CLIENT_ID=YOUR_GOOGLE_OAUTH_WEB_CLIENT_ID
```

Email/password auth works once Email is enabled in Supabase Auth. For Google sign-in, enable Google under Supabase Auth → Providers and configure the OAuth client there; set only the OAuth client ID in `GOOGLE_WEB_CLIENT_ID`. The provider client secret belongs in Supabase settings, never in Android or GitHub Actions.

Do not commit `.env`, local Gradle properties, access tokens, database passwords, service-role keys, or Google OAuth client secrets.

## Deploy with GitHub Actions

`.github/workflows/deploy-supabase.yml` runs when Supabase files change on `main`, or when manually dispatched. It links the project, applies versioned migrations, and deploys `reloop-api`.

Add these GitHub repository Actions secrets before merging the deployment workflow:

| Secret | Where to get it |
|---|---|
| `SUPABASE_ACCESS_TOKEN` | Supabase account settings → Access Tokens |
| `SUPABASE_DB_PASSWORD` | The database password chosen when creating the Supabase project |
| `SUPABASE_PROJECT_ID` | The project reference in the Supabase dashboard URL |

Use a dedicated Supabase project for production. Review SQL migrations before merging because they change the hosted database. The workflow only applies checked-in migrations and deploys the function; it does not delete the project or erase Firebase data.

## Firebase data and account migration

The Android source no longer uses Firebase, but data in a hosted Firebase project is not deleted or exported by source-code changes. Use the one-time importer in `backend/migration/` after deploying the Supabase schema and REST function:

1. In Firebase, create a read-only service account for Authentication and Firestore, download its JSON key to a protected location outside the repository, and keep a separate Firestore backup.
2. In Supabase, copy the project URL and server-side secret key. Keep the secret key out of the Android app and repository.
3. Install the importer's dependencies from `backend/migration` with `npm install`.
4. In PowerShell, set `FIREBASE_SERVICE_ACCOUNT_PATH` to the protected JSON file, `SUPABASE_URL` to the project URL, and `SUPABASE_SECRET_KEY` to the Supabase server-side secret key. Then run `npm run migrate` from `backend/migration`.
5. The importer reads the Firebase Auth users and listed Firestore collections, creates Supabase Auth users with random passwords, maps Firebase UIDs to new Supabase UUIDs, and upserts records in dependency order. It does not delete or modify Firebase data. Existing users will need to reset their passwords after cutover.
6. Compare source and destination row counts, sample profiles/pickups/reward ledgers, test login and RLS in the Android app, and keep the backup. Only after that verification should you retire the Firebase project from Firebase Console.

The importer is idempotent for table records and matches already-existing destination accounts by email. Do not run it against production until you have reviewed the schema and have a restorable Firebase export.

The Firebase Cloud Messaging integration has been removed. Notifications still persist in the Supabase inbox, but device push delivery is not configured. If device push is required, choose and configure a push provider separately; Supabase Edge Functions do not deliver mobile push notifications by themselves.
