# Chalchitra

A wallpaper app for people who think a 40 MB wallpaper app is a personality flaw.

Pulls backgrounds from Bing's daily image and a handful of Reddit communities, so you can
spend less time scrolling and more time pretending you chose that sunset yourself.

## What it does

- **Tiny.** No Coil, Retrofit, WorkManager or other "essential" libraries. Android already ships HTTP, JSON and a job scheduler. Revolutionary.
- **Material 3 Expressive.** Dynamic colour, springy motion, pure-black AMOLED dark theme.
- **Sources:** Bing daily, r/MobileWallpaper, r/phonewallpapers, r/iWallpaper, r/Amoledbackgrounds, r/Verticalwallpapers, r/androidthemes, r/Homescreens, r/AnimePhoneWallpapers, r/MinimalWallpaper, r/EarthPorn, r/SpacePorn. No live wallpapers: video loops are a battery tax, and we're against taxes.
- **Auto changer.** One `JobScheduler` job, every 15 min to 24 h, only with network and battery not low. Zero services running while you aren't looking.
- **Status bar dim.** A soft gradient baked into the wallpaper top, so the clock survives your questionable taste in bright skies.
- **Low memory.** Only the screen-sized centre crop of each image is decoded, so a 5000 px mountain doesn't eat your RAM for breakfast.

## Build

CI does it. Your phone has suffered enough.

```bash
gh secret set KEYSTORE_B64 < <(base64 -w0 ~/your-release.jks)
gh secret set KEYSTORE_PASSWORD
gh secret set KEY_ALIAS
gh secret set KEY_PASSWORD
git tag v1.0.0 && git push --tags
gh run watch
gh release download v1.0.0 -p '*.apk'
```

No secrets set? It signs with the debug key. It will install. It will judge you silently.

## Caveats

- Reddit occasionally rate-limits anonymous requests. Retry, or go touch grass for a minute.
- Landscape subs are centre-cropped. Your mountain may lose a peak. Mountains are resilient.
- NSFW posts are always skipped. Always.

## License

Pick one yourself (GPL-3.0 is a good FOSS default). The author is allergic to paperwork.
