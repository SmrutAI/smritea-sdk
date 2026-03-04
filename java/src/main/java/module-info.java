module ai.smritea.sdk {
    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.annotation;

    exports ai.smritea.sdk;
    exports ai.smritea.sdk.errors;
    exports ai.smritea.sdk.model;
}
