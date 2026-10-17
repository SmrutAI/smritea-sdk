// <copyright file="SmriteaNotFoundException.cs" company="PlaceholderCompany">
// Copyright (c) PlaceholderCompany. All rights reserved.
// </copyright>

namespace Smritea.Sdk;

/// <summary>Raised on HTTP 404 — requested resource not found.</summary>
public class SmriteaNotFoundException : SmriteaException
{
    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaNotFoundException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="statusCode">The HTTP status code.</param>
    /// <param name="code">The machine-readable wire code from the server response.</param>
    /// <param name="retryable">Whether the server marked this error as retryable.</param>
    /// <param name="body">The full parsed JSON response body, if available.</param>
    /// <param name="requestId">The server-assigned request ID for the failed call, if available.</param>
    public SmriteaNotFoundException(string message, int statusCode, string? code = null, bool retryable = false, object? body = null, string? requestId = null)
        : base(message, statusCode, code, retryable, body, requestId)
    {
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaNotFoundException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="statusCode">The HTTP status code, if available.</param>
    /// <param name="code">The machine-readable wire code from the server response.</param>
    /// <param name="retryable">Whether the server marked this error as retryable.</param>
    /// <param name="body">The full parsed JSON response body, if available.</param>
    /// <param name="requestId">The server-assigned request ID for the failed call, if available.</param>
    public SmriteaNotFoundException(string message, int? statusCode = null, string? code = null, bool retryable = false, object? body = null, string? requestId = null)
        : base(message, statusCode, code, retryable, body, requestId)
    {
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaNotFoundException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="statusCode">The HTTP status code.</param>
    /// <param name="innerException">The inner exception.</param>
    public SmriteaNotFoundException(string message, int? statusCode, Exception innerException)
        : base(message, statusCode, innerException)
    {
    }

    /// <summary>Initializes a new instance of the <see cref="SmriteaNotFoundException"/> class.</summary>
    public SmriteaNotFoundException()
        : base()
    {
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaNotFoundException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    public SmriteaNotFoundException(string? message)
        : base(message)
    {
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaNotFoundException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="innerException">The inner exception.</param>
    public SmriteaNotFoundException(string? message, Exception? innerException)
        : base(message, innerException)
    {
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaNotFoundException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="statusCode">The HTTP status code, if available.</param>
    /// <param name="innerException">The inner exception.</param>
    /// <param name="retryable">Whether the server marked this error as retryable.</param>
    /// <param name="body">The full parsed JSON response body, if available.</param>
    public SmriteaNotFoundException(string message, int? statusCode, Exception innerException, bool retryable = false, object? body = null)
        : base(message, statusCode, innerException, retryable, body)
    {
    }
}
