// <copyright file="SmriteaRateLimitException.cs" company="PlaceholderCompany">
// Copyright (c) PlaceholderCompany. All rights reserved.
// </copyright>

namespace Smritea.Sdk;

/// <summary>Raised on HTTP 429 — rate limit exceeded.</summary>
public class SmriteaRateLimitException : SmriteaException
{
    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaRateLimitException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="statusCode">The HTTP status code.</param>
    /// <param name="retryAfter">Seconds to wait before retrying, from the Retry-After header.</param>
    public SmriteaRateLimitException(string message, int statusCode, int? retryAfter = null)
        : base(message, statusCode)
    {
        this.RetryAfter = retryAfter;
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaRateLimitException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="statusCode">The HTTP status code, if available.</param>
    public SmriteaRateLimitException(string message, int? statusCode = null)
        : base(message, statusCode)
    {
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaRateLimitException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="statusCode">The HTTP status code.</param>
    /// <param name="innerException">The inner exception.</param>
    public SmriteaRateLimitException(string message, int? statusCode, Exception innerException)
        : base(message, statusCode, innerException)
    {
    }

    /// <summary>Initializes a new instance of the <see cref="SmriteaRateLimitException"/> class.</summary>
    public SmriteaRateLimitException()
        : base()
    {
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaRateLimitException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    public SmriteaRateLimitException(string? message)
        : base(message)
    {
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaRateLimitException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="innerException">The inner exception.</param>
    public SmriteaRateLimitException(string? message, Exception? innerException)
        : base(message, innerException)
    {
    }

    /// <summary>
    /// Gets the number of seconds to wait before retrying, or null if not provided by the server.
    /// </summary>
    public int? RetryAfter { get; }
}
