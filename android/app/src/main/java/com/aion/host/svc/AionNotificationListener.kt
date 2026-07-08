package com.aion.host.svc

import android.service.notification.NotificationListenerService

/**
 * PR-02 (DOC-002 §9) — declared so AION appears as a grantable option under
 * Settings > Notification access. No notification content is read yet; the ingestion +
 * InjectionFilter wrapping for automation context (DOC-004 §6, DOC-009) is unscoped future
 * work — see BACKLOG.md. This class exists only to make the permission itself real and
 * testable for T-004.
 */
class AionNotificationListener : NotificationListenerService()
