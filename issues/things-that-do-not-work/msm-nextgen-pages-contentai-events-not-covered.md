# MSM, next-gen Pages/Sites OpenAPIs, Content AI, Events, and Forms document APIs not covered

**Status:** does not work (coverage gap)
**Affects:** no command — AEMaaCS
**Why:** Adobe's developer portal (https://developer.adobe.com/experience-cloud/experience-manager-apis/)
documents these as first-class APIs with OpenAPI specs; the CLI implements
none of them:
- Multisite Manager (experimental): live copies, rollouts
  (…/apis/api/experimental/sites/msm/)
- Site Management + Page Management (experimental, next-gen beyond
  `/api/pages`)
- Content AI API: GenStudio-style content AI operations
- AEM Events: eventing/journaling
- Forms Document Generation/Manipulation (Cloud Service)
**Symptoms:** all of these operations require the author UI, curl, or other
tooling.
**Workaround:** none in this tool; use the Adobe-documented endpoints
directly or via Cloud Manager/App Builder.
