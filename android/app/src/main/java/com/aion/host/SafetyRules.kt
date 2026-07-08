package com.aion.host

/** SR-01: no side-effect action executes without explicit user approval.
 *  SR-03: kill phrase halts automation <1s.  SR-04: hash-chained audit log.
 *  Built FIRST, before any automation capability (DOC-020 §5). */
object SafetyRules {
    val SIDE_EFFECT_ACTIONS = setOf("send", "post", "pay", "purchase", "install", "delete", "deploy")
    const val KILL_PHRASE = "aion stop"
}
