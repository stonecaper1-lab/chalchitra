# Chalchitra

A wallpaper app for people who think a 40 MB wallpaper app is a personality flaw.

Pulls backgrounds from Bing's daily image and a handful of Reddit communities, so you can
spend less time scrolling and more time pretending you chose that sunset yourself.

## What it does

- **Tiny.** No Coil, Retrofit, WorkManager or other "essential" libraries. Android already ships HTTP, JSON and a job scheduler. Revolutionary.
- **Expressive look.** Dynamic colour, chunky rounded corners, bouncy press animations, pure-black AMOLED dark theme. Not the official expressive components: those need AGP 9 and compileSdk 37, so we faked it with taste.
- **Sources:** Bing daily, Wallhaven (top, dark, anime), Picsum photos, NASA APOD, Art Institute of Chicago public-domain art, two GitHub wallpaper repos (D3Ext/aesthetic-wallpapers and michaelScopic/Wallpapers, in several categories), plus r/MobileWallpaper, r/phonewallpapers, r/iWallpaper, r/Amoledbackgrounds, r/Verticalwallpapers, r/androidthemes, r/Homescreens, r/AnimePhoneWallpapers, r/MinimalWallpaper, r/EarthPorn, r/SpacePorn. No live wallpapers: video loops are a battery tax, and we're against taxes.
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

- Reddit increasingly blocks anonymous clients with a 403. The app tries three hosts with a browser user agent, then gives up gracefully. That is why the non-Reddit sources exist. Thanks, Reddit.
- GitHub sources list a repo once through the GitHub API (60 requests/hour, plenty) and resize images through the free wsrv.nl service, because some wallpapers weigh 45 MB and your data plan has feelings.
- NASA APOD uses the shared `DEMO_KEY`. Put your own free key in `Feed.kt` if it gets cranky.
- Landscape subs are centre-cropped. Your mountain may lose a peak. Mountains are resilient.
- NSFW posts are always skipped. Always.

## Credits

GitHub-sourced images belong to their original creators (both repos credit r/unixporn, Wallhaven and friends). Thumbnails for those go through the free wsrv.nl resizer so your data plan survives. Reddit wallpapers belong to the people who posted them.

## License

Pick one yourself (GPL-3.0 is a good FOSS default). The author is allergic to paperwork.
