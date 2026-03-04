namespace Smritea.Sdk;

/// <summary>Base exception for all smritea SDK errors.</summary>
public class SmriteaException : Exception
{
    public int? StatusCode { get; }

    public SmriteaException(string message, int? statusCode = null)
        : base(message)
    {
        StatusCode = statusCode;
    }

    public SmriteaException(string message, int? statusCode, Exception innerException)
        : base(message, innerException)
    {
        StatusCode = statusCode;
    }
}

/// <summary>Raised on HTTP 401 — invalid or missing API key.</summary>
public class SmriteaAuthException : SmriteaException
{
    public SmriteaAuthException(string message, int statusCode)
        : base(message, statusCode) { }
}

/// <summary>Raised on HTTP 400 — request validation failed.</summary>
public class SmriteaValidationException : SmriteaException
{
    public SmriteaValidationException(string message, int statusCode)
        : base(message, statusCode) { }
}

/// <summary>Raised on HTTP 404 — requested resource not found.</summary>
public class SmriteaNotFoundException : SmriteaException
{
    public SmriteaNotFoundException(string message, int statusCode)
        : base(message, statusCode) { }
}

/// <summary>Raised on HTTP 402 — quota or payment required.</summary>
public class SmriteaQuotaException : SmriteaException
{
    public SmriteaQuotaException(string message, int statusCode)
        : base(message, statusCode) { }
}

/// <summary>Raised on HTTP 429 — rate limit exceeded.</summary>
public class SmriteaRateLimitException : SmriteaException
{
    /// <summary>
    /// Gets the number of seconds to wait before retrying, or null if not provided by the server.
    /// </summary>
    public int? RetryAfter { get; }

    public SmriteaRateLimitException(string message, int statusCode, int? retryAfter = null)
        : base(message, statusCode)
    {
        RetryAfter = retryAfter;
    }
}
