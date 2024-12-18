package gov.cdc.ocio.processingstatusapi.models

/**
 * Action enumeration for ingesting reports, which is to either create a new report or replace an existing one.
 */
enum class Action {
    CREATE, REPLACE
}