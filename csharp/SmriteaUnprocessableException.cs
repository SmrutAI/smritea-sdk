// <copyright file="SmriteaUnprocessableException.cs" company="PlaceholderCompany">
// Copyright (c) PlaceholderCompany. All rights reserved.
// </copyright>

namespace Smritea.Sdk;

/// <summary>Raised on HTTP 422 — the request was well-formed but semantically invalid.</summary>
public class SmriteaUnprocessableException : SmriteaException
{
    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaUnprocessableException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="statusCode">The HTTP status code.</param>
    /// <param name="code">The machine-readable wire code from the server response.</param>
    /// <param name="retryable">Whether the server marked this error as retryable.</param>
    /// <param name="body">The full parsed JSON response body, if available.</param>
    public SmriteaUnprocessableException(string message, int statusCode, string? code = null, bool retryable = false, object? body = null)
        : base(message, statusCode, code, retryable, body)
    {
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaUnprocessableException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="statusCode">The HTTP status code, if available.</param>
    /// <param name="code">The machine-readable wire code from the server response.</param>
    /// <param name="retryable">Whether the server marked this error as retryable.</param>
    /// <param name="body">The full parsed JSON response body, if available.</param>
    public SmriteaUnprocessableException(string message, int? statusCode = null, string? code = null, bool retryable = false, object? body = null)
        : base(message, statusCode, code, retryable, body)
    {
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaUnprocessableException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="statusCode">The HTTP status code.</param>
    /// <param name="innerException">The inner exception.</param>
    public SmriteaUnprocessableException(string message, int? statusCode, Exception innerException)
        : base(message, statusCode, innerException)
    {
    }

    /// <summary>Initializes a new instance of the <see cref="SmriteaUnprocessableException"/> class.</summary>
    public SmriteaUnprocessableException()
        : base()
    {
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaUnprocessableException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    public SmriteaUnprocessableException(string? message)
        : base(message)
    {
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaUnprocessableException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="innerException">The inner exception.</param>
    public SmriteaUnprocessableException(string? message, Exception? innerException)
        : base(message, innerException)
    {
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaUnprocessableException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="statusCode">The HTTP status code, if available.</param>
    /// <param name="innerException">The inner exception.</param>
    /// <param name="body">The full parsed JSON response body, if available.</param>
    public SmriteaUnprocessableException(string message, int? statusCode, Exception innerException, object? body = null)
        : base(message, statusCode, innerException, false, body)
    {
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaUnprocessableException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="statusCode">The HTTP status code, if available.</param>
    /// <param name="innerException">The inner exception.</param>
    /// <param name="retryable">Whether the server marked this error as retryable.</param>
    /// <param name="body">The full parsed JSON response body, if available.</param>
    public SmriteaUnprocessableException(string message, int? statusCode, Exception innerException, bool retryable = false, object? body = null)
        : base(message, statusCode, innerException, retryable, body)
    {
    }
}
