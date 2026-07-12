package com.aion.brain

enum class BenchmarkCategory { MESSAGING, SETTINGS, CALENDAR, MAPS, MEDIA, FINANCE_READONLY }

data class BenchmarkTask(
    val goal: String,
    val category: BenchmarkCategory,
)

/**
 * DOC-001 §12 — "≥60% task success on a 50-task internal Hindi+English benchmark (messaging,
 * settings, calendar, maps, media, read-only finance)." Real, plausible goals a Hindi/English-
 * speaking owner would actually give AION, matching the Hinglish style already established
 * throughout this project's own test fixtures (T-050's `PlannerAgentTest`, T-090's skill goals).
 *
 * Not curated to dodge known gaps: several of these (anything needing typed text — a message body,
 * an event title, a search query) are expected to fail given DispatcherActionExecutor only
 * implements tap/longPress/launchApp/globalAction today (T-121 finding, PlanStep has no field for
 * typed text, BACKLOG.md). Including them anyway is the honest reading of "evaluated honestly" —
 * a benchmark that only asks what's already known to work isn't measuring anything.
 */
val BENCHMARK_TASKS =
    listOf(
        // MESSAGING (8) — several need typed input, expected to fail structurally today.
        BenchmarkTask("send a message to Ravi saying running late", BenchmarkCategory.MESSAGING),
        BenchmarkTask("WhatsApp khol ke naya message bhejo", BenchmarkCategory.MESSAGING),
        BenchmarkTask("reply to the last message saying ok", BenchmarkCategory.MESSAGING),
        BenchmarkTask("messages app kholo", BenchmarkCategory.MESSAGING),
        BenchmarkTask("unread messages check karo", BenchmarkCategory.MESSAGING),
        BenchmarkTask("mummy ko call karo", BenchmarkCategory.MESSAGING),
        BenchmarkTask("dialer kholo", BenchmarkCategory.MESSAGING),
        BenchmarkTask("missed calls check karo", BenchmarkCategory.MESSAGING),
        // SETTINGS (9) — mostly tap/launchApp only, expected to be the strongest category.
        BenchmarkTask("wifi on karo", BenchmarkCategory.SETTINGS),
        BenchmarkTask("bluetooth band karo", BenchmarkCategory.SETTINGS),
        BenchmarkTask("dark mode on karo", BenchmarkCategory.SETTINGS),
        BenchmarkTask("battery percentage batao", BenchmarkCategory.SETTINGS),
        BenchmarkTask("screen brightness settings kholo", BenchmarkCategory.SETTINGS),
        BenchmarkTask("storage space check karo", BenchmarkCategory.SETTINGS),
        BenchmarkTask("notification settings kholo", BenchmarkCategory.SETTINGS),
        BenchmarkTask("airplane mode on karo", BenchmarkCategory.SETTINGS),
        BenchmarkTask("settings kholo", BenchmarkCategory.SETTINGS),
        // CALENDAR (8) — event creation needs typed input, viewing doesn't.
        BenchmarkTask("calendar app kholo", BenchmarkCategory.CALENDAR),
        BenchmarkTask("aaj ke events dikhao", BenchmarkCategory.CALENDAR),
        BenchmarkTask("kal ka schedule check karo", BenchmarkCategory.CALENDAR),
        BenchmarkTask("add a new event tomorrow at 5pm called meeting", BenchmarkCategory.CALENDAR),
        BenchmarkTask("next week ka calendar dekho", BenchmarkCategory.CALENDAR),
        BenchmarkTask("set a reminder for the doctor appointment", BenchmarkCategory.CALENDAR),
        BenchmarkTask("is week ke events count karo", BenchmarkCategory.CALENDAR),
        BenchmarkTask("calendar mein aaj jao", BenchmarkCategory.CALENDAR),
        // MAPS (8) — search/navigation-by-name needs typed input, opening the app doesn't.
        BenchmarkTask("google maps kholo", BenchmarkCategory.MAPS),
        BenchmarkTask("current location check karo", BenchmarkCategory.MAPS),
        BenchmarkTask("search for nearest coffee shop", BenchmarkCategory.MAPS),
        BenchmarkTask("traffic check karo maps mein", BenchmarkCategory.MAPS),
        BenchmarkTask("navigate home", BenchmarkCategory.MAPS),
        BenchmarkTask("distance to office check karo", BenchmarkCategory.MAPS),
        BenchmarkTask("maps mein saved places dekho", BenchmarkCategory.MAPS),
        BenchmarkTask("nearby petrol pump dhundo", BenchmarkCategory.MAPS),
        // MEDIA (9) — mostly launch/tap, expected to do reasonably well.
        BenchmarkTask("youtube kholo", BenchmarkCategory.MEDIA),
        BenchmarkTask("music play karo", BenchmarkCategory.MEDIA),
        BenchmarkTask("gallery kholo", BenchmarkCategory.MEDIA),
        BenchmarkTask("camera se photo lo", BenchmarkCategory.MEDIA),
        BenchmarkTask("camera kholo", BenchmarkCategory.MEDIA),
        BenchmarkTask("video pause karo", BenchmarkCategory.MEDIA),
        BenchmarkTask("volume badhao", BenchmarkCategory.MEDIA),
        BenchmarkTask("next song play karo", BenchmarkCategory.MEDIA),
        BenchmarkTask("lofi music youtube pe dhundo", BenchmarkCategory.MEDIA),
        // FINANCE_READONLY (8) — no finance app on a bare emulator; expected to fail on missing app.
        BenchmarkTask("bank balance check karo", BenchmarkCategory.FINANCE_READONLY),
        BenchmarkTask("google pay kholo", BenchmarkCategory.FINANCE_READONLY),
        BenchmarkTask("recent transactions dikhao", BenchmarkCategory.FINANCE_READONLY),
        BenchmarkTask("UPI balance check karo", BenchmarkCategory.FINANCE_READONLY),
        BenchmarkTask("paytm kholo", BenchmarkCategory.FINANCE_READONLY),
        BenchmarkTask("is mahine kitna kharch hua dikhao", BenchmarkCategory.FINANCE_READONLY),
        BenchmarkTask("check credit card statement", BenchmarkCategory.FINANCE_READONLY),
        BenchmarkTask("wallet balance dikhao", BenchmarkCategory.FINANCE_READONLY),
    )
