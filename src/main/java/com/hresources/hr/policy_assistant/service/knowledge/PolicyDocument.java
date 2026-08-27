package com.hresources.hr.policy_assistant.service.knowledge;

import java.util.List;

/**
 * Represents a single HR policy document entry stored in the local knowledge base.
 *
 * @param id stable document identifier
 * @param title human-readable document title
 * @param source source label exposed to API consumers
 * @param content searchable body text used during retrieval
 */
public record PolicyDocument(
        String id,
        String title,
        String source,
        String content) {
}
