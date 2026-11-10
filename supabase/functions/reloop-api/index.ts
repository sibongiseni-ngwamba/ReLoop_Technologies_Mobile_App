import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
  "Access-Control-Allow-Methods": "GET, POST, PUT, PATCH, DELETE, OPTIONS",
};

const json = (body: unknown, status = 200) => new Response(JSON.stringify(body), {
  status,
  headers: { ...corsHeaders, "Content-Type": "application/json" },
});

const toSnake = (value: unknown): unknown => {
  if (Array.isArray(value)) return value.map(toSnake);
  if (value && typeof value === "object") {
    return Object.fromEntries(Object.entries(value).map(([key, item]) => [
      key.replace(/[A-Z]/g, (letter) => `_${letter.toLowerCase()}`),
      toSnake(item),
    ]));
  }
  return value;
};

const normalizeDates = (value: unknown): unknown => {
  if (Array.isArray(value)) return value.map(normalizeDates);
  if (value && typeof value === "object") {
    return Object.fromEntries(Object.entries(value).map(([key, item]) => {
      if (/(?:_at|_date)$/.test(key) && typeof item === "number") return [key, new Date(item).toISOString()];
      return [key, normalizeDates(item)];
    }));
  }
  return value;
};

const toCamel = (value: unknown): unknown => {
  if (Array.isArray(value)) return value.map(toCamel);
  if (value && typeof value === "object") {
    return Object.fromEntries(Object.entries(value).map(([key, item]) => {
      let converted = item;
      if (typeof item === "string" && /(?:_at|_date)$/.test(key)) {
        const parsed = Date.parse(item);
        if (!Number.isNaN(parsed)) converted = parsed;
      }
      return [key.replace(/_([a-z])/g, (_, letter) => letter.toUpperCase()), toCamel(converted)];
    }));
  }
  return value;
};

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (request.method === "GET" && new URL(request.url).pathname.endsWith("/health")) {
    return json({ status: "ok", service: "reloop-api" });
  }

  const url = new URL(request.url);
  const path = url.pathname.replace(/^\/functions\/v1\/reloop-api\/?/, "").split("/").filter(Boolean);
  const token = request.headers.get("Authorization")?.replace(/^Bearer\s+/i, "");
  if (!token) return json({ error: "Authentication required." }, 401);

  const supabase = createClient(Deno.env.get("SUPABASE_URL")!, Deno.env.get("SUPABASE_ANON_KEY")!, {
    global: { headers: { Authorization: `Bearer ${token}` } },
    auth: { persistSession: false, autoRefreshToken: false },
  });
  const { data: authData, error: authError } = await supabase.auth.getUser(token);
  if (authError || !authData.user) return json({ error: "Invalid or expired session." }, 401);
  const userId = authData.user.id;
  const method = request.method;
  const body = method === "GET" || method === "DELETE" ? {} : (normalizeDates(toSnake(await request.json())) as Record<string, unknown>);

  try {
    let result: any;
    if (path[0] === "users" && path[1]) {
      if (path[1] !== userId) return json({ error: "You cannot access another user's profile." }, 403);
      if (method === "GET") result = await supabase.from("profiles").select("*").eq("id", userId).single();
      else if (method === "POST" || method === "PUT" || method === "PATCH") {
        const { user_id: _ignoredUserId, role: _ignoredRole, is_active: _ignoredActive, ...profileFields } = body;
        result = await supabase.from("profiles").upsert({ ...profileFields, id: userId, email: authData.user.email ?? "" }).select("*").single();
      }
    } else if (path[0] === "pickups") {
      if (path[1] && method === "GET") result = await supabase.from("pickups").select("*").eq("pickup_id", path[1]).single();
      else if (path[1] && (method === "PUT" || method === "PATCH")) result = await supabase.from("pickups").update(body).eq("pickup_id", path[1]).select("*").single();
      else if (!path[1] && method === "GET") result = await supabase.from("pickups").select("*").order("created_at", { ascending: false });
      else if (!path[1] && method === "POST") result = await supabase.from("pickups").insert({ ...(body as object), user_id: userId }).select("*").single();
    } else if (path[0] === "rewards" && path[1] === "account" && method === "GET") {
      result = await supabase.from("reward_accounts").select("*").eq("user_id", userId).single();
    } else if (path[0] === "rewards" && path[1] === "items" && method === "GET") {
      result = await supabase.from("reward_items").select("*").eq("is_active", true).order("points_cost");
    } else if (path[0] === "rewards" && path[1] === "transactions" && method === "GET") {
      result = await supabase.from("points_transactions").select("*").eq("user_id", userId).order("created_at", { ascending: false });
    } else if (path[0] === "rewards" && path[1] === "transactions" && method === "POST") {
      result = await supabase.rpc("award_points", {
        p_points: Number(body.points ?? 0),
        p_related_id: String(body.related_id ?? "") || null,
        p_description: String(body.description ?? ""),
      });
    } else if (path[0] === "rewards" && path[1] === "redemptions" && method === "POST") {
      result = await supabase.rpc("redeem_reward", { p_reward_item_id: String(body.reward_item_id ?? "") });
    } else if (path[0] === "waste-categories" && method === "GET") {
      result = await supabase.from("waste_categories").select("*").eq("is_active", true).order("category_name");
    } else if (path[0] === "waste-logs" && method === "GET") {
      result = await supabase.from("waste_logs").select("*").eq("user_id", userId).order("logged_at", { ascending: false });
    } else if (path[0] === "waste-logs" && method === "POST") {
      result = await supabase.from("waste_logs").insert({ ...(body as object), user_id: userId }).select("*").single();
    } else if (path[0] === "notifications" && method === "GET") {
      result = await supabase.from("notifications").select("*").eq("user_id", userId).order("sent_at", { ascending: false });
    } else if (path[0] === "notifications" && method === "POST") {
      result = await supabase.from("notifications").insert({ ...(body as object), user_id: userId }).select("*").single();
    } else if (path[0] === "notifications" && path[1] && (method === "PATCH" || method === "PUT")) {
      result = await supabase.from("notifications").update(body).eq("notification_id", path[1]).eq("user_id", userId).select("*").single();
    } else if (path[0] === "notifications" && path[1] && method === "DELETE") {
      result = await supabase.from("notifications").delete().eq("notification_id", path[1]).eq("user_id", userId);
    } else if (path[0] === "notifications" && method === "DELETE") {
      result = await supabase.from("notifications").delete().eq("user_id", userId);
    } else if (path[0] === "guidance" && method === "GET") {
      result = await supabase.from("guidance_content").select("*").eq("is_active", true).order("title");
    } else {
      return json({ error: "Route not found." }, 404);
    }

    if (result?.error) {
      const status = result.error.code === "PGRST116" ? 404 : 400;
      return json({ error: result.error.message }, status);
    }
    const responseData = method === "DELETE"
      ? { ok: true }
      : path[0] === "users" && result?.data
      ? { ...result.data, user_id: result.data.id }
      : result?.data ?? null;
    return json(toCamel(responseData));
  } catch (error) {
    console.error("reloop-api request failed", error);
    return json({ error: "The request could not be completed." }, 500);
  }
});
