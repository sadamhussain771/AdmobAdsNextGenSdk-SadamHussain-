# AdmobAdsNextGenSdk

A modular Android SDK for integrating and managing **Google AdMob Next Gen ads** with preloading support, consent management, billing, and remote config — built with Kotlin.

## Modules

| Module | Description |
|---|---|
| `sdkAdsNextGen` | Core ads SDK — handles preloading and showing all ad formats |
| `core` | Shared utilities, DI base, and common extensions |
| `app` | Sample app demonstrating SDK integration |

## Ad Formats Supported

- **Banner Ads**
- **Interstitial Ads**
- **Native Ads** (Large & Small templates)
- **App Open Ads**
- **Rewarded Interstitial Ads**

## Tech Stack

- **Language:** Kotlin
- **Min SDK:** 29 | **Target SDK:** 36
- **AdMob Next Gen SDK** (`ads-mobile-sdk`)
- **Firebase** — Analytics, Crashlytics, Remote Config
- **Google UMP** — User consent management
- **Koin** — Dependency injection
- **Google Play Billing**
- **Navigation Component**
- **Shimmer** — Native ad loading skeleton
- **Lottie** — Animations
