package org.rooftop.netx.api

enum class SuccessWith {
    /**
     * Change the saga to the "Join" state and publish an event.
     */
    PUBLISH_JOIN,

    /**
     * Change the saga to the "Commit" state and publish an event.
     */
    PUBLISH_COMMIT,

    /**
     * Terminate the saga without further progression.
     */
    END
}
