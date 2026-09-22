# Public access without buying a domain

Expose **only** the PHI stack (API + web + APK downloads) to the internet.  
Your home LAN, SSH, and Proxmox stay private.

## Recommended: Tailscale Funnel — **static** free URL

Your permanent URL (does not change):

```
https://edge-server.tail8a02ee.ts.net
```

If Firefox says **Server Not Found**, HTTPS certificates are not enabled on your tailnet yet.

### One-time fix (Tailscale admin, ~2 min)

1. https://login.tailscale.com/admin/dns → enable **MagicDNS** + **HTTPS certificates**
2. https://login.tailscale.com/admin/acls → **Add Funnel to policy**
3. On edge:

```bash
ssh edge
sh /opt/phi-edge/fix-tailscale-public-url.sh
```

### Why not Cloudflare Quick Tunnel?

Quick Tunnel (`*.trycloudflare.com`) works immediately but the **URL changes every restart** — bad for sharing APK links. Use it only as a temporary test:

```bash
sh /opt/phi-edge/install-cloudflare-tunnel.sh   # ephemeral URL
```

## Tailscale Funnel details

| | |
|---|---|
| **Cost** | Free (personal Tailscale account) |
| **URL** | `https://edge-server.your-tailnet.ts.net` |
| **Exposes** | Only Caddy `:80` (you choose what Caddy serves) |
| **Does not expose** | Other LAN devices, SSH, router admin |

### One-time on edge

```bash
ssh -t edge
sh /opt/phi-edge/install-tailscale-funnel.sh
# First time: tailscale up → open auth link → re-run script
```

The script prints your public URL and saves it to `/etc/phi/public-url`.

### What testers get

| Link | Purpose |
|------|---------|
| `https://….ts.net/` | Web app |
| `https://….ts.net/downloads/` | APK download page |
| `https://….ts.net/api/v1/health` | API |

## Alternatives (also free, no domain)

### Cloudflare Quick Tunnel

```bash
# On edge — ephemeral URL, good for quick tests
apk add cloudflared
cloudflared tunnel --url http://127.0.0.1:80
```

Gives `https://random-words.trycloudflare.com` (changes when restarted).

### Tailscale private (no public URL)

Testers install Tailscale and join your tailnet. They use `http://edge-server` on the tailnet.  
Most private; each tester needs a Tailscale account.

## Deploy clients after Funnel is up

```bash
# Backend (unchanged)
./infrastructure/ci/publish-to-edge.sh

# Web
PUBLIC_URL=$(ssh edge cat /etc/phi/public-url)
./infrastructure/ci/publish-web-to-edge.sh

# Android APK (first time: npm i -g eas-cli && eas login && cd mobile && eas init)
PUBLIC_URL=$PUBLIC_URL ./infrastructure/ci/build-android-apk.sh
# After EAS build finishes:
./infrastructure/ci/publish-apk-to-edge.sh ~/Downloads/build.apk
```

### OTA updates (no new APK)

After testers install the APK once:

```bash
cd mobile
eas update --channel preview --message "fix login screen"
```

App checks for updates on launch (`expo-updates`).

## Security notes

- Funnel URL is **public** — anyone with the link can hit your API. App login (JWT) still required for data.
- Do not share your Tailscale admin or SSH keys.
- Prefer Funnel over port-forwarding your whole router.
