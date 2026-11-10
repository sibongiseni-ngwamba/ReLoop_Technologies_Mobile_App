# ReLoop Technologies Mobile App

Android app for ReLoop Technologies SA, built with Kotlin. It supports recycling pickup scheduling, waste classification, reward tracking, recycling guidance, and notifications.

## Backend

The app uses Supabase Auth and PostgreSQL, with business data accessed through an authenticated REST API implemented as a Supabase Edge Function. GitHub Actions applies SQL migrations and deploys the API. A Supabase project and credentials must be configured before running the app or deploying.

See the [Backend README](backend/README.md) for the database schema, REST routes, local development, migration notes, and deployment setup.

## Android setup

- Android Studio and the Android SDK for compile SDK 37
- JDK 11 or the Gradle toolchain configured by the project
- A Supabase project (or local Supabase CLI stack)

Supabase URL and public anon/publishable key are supplied through local-only Gradle properties. Never put a Supabase service-role key in the Android app. The Android app must use the REST API and the signed-in user's access token for business data.

## Database and API deployment

Migrations live under `supabase/migrations/`; the REST Edge Function lives under `supabase/functions/reloop-api/`. GitHub Actions applies migrations and deploys the function on pushes to `main` that change Supabase files. Set the required GitHub Actions secrets as described in [backend/README.md](backend/README.md) before enabling deployment.

## Existing Firebase project

The Android source no longer depends on Firebase. This does not delete records or accounts from the hosted Firebase project. Export and verify live records before retiring Firebase; account passwords cannot be migrated as ordinary database values. The Firebase push service was removed, so notifications currently appear in the in-app inbox. See the [migration notes](backend/README.md#firebase-data-and-account-migration).
