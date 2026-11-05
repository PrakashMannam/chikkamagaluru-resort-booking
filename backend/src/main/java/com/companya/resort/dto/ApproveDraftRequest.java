package com.companya.resort.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ApproveDraftRequest {

    @JsonProperty("draft_body")
    @JsonAlias({"draft_body", "draftBody"})
    private String draftBody;

    public ApproveDraftRequest() {
    }

    public ApproveDraftRequest(String draftBody) {
        this.draftBody = draftBody;
    }

    public String getDraftBody() {
        return draftBody;
    }

    public void setDraftBody(String draftBody) {
        this.draftBody = draftBody;
    }
}
