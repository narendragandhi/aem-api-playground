# PackMgr runs on the AEM as a Cloud Service author

**Status:** fixed (works, with platform restrictions)
**Affects:** `packages list/get/upload/build/download/rm` — AEM as a Cloud Service author
**Why:** the Package Manager surface works unchanged on the Cloud author
service using the IMS Bearer auth path (`connect --access-token ... --https-only`).
`install`/`uninstall` remain **restricted** by the platform: permitted in
dev/RDE, blocked in higher tiers, and packages modifying OSGi configurations
or security ACLs are rejected.
**Symptoms:** none for build/download flows (e.g. the cloud-safe
`content-backup` recipe runs on Cloud today). For installs, prefer Cloud
Manager pipelines.
