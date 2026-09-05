// <copyright file="SmriteaException.cs" company="PlaceholderCompany">
// Copyright (c) PlaceholderCompany. All rights reserved.
// </copyright>

namespace Smritea.Sdk;

/// <summary>
/// Base exception for all smritea SDK errors. Also raised directly for 5xx / unknown HTTP
/// statuses that do not have a dedicated subclass.
/// </summary>
public class SmriteaException : Exception
{
    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="statusCode">The HTTP status code, if available.</param>
    /// <param name="code">The machine-readable wire code from the server response (e.g. <c>MEMORY_NOT_FOUND</c>).</param>
    /// <param name="retryable">Whether the server marked this error as retryable.</param>
    /// <param name="body">The full parsed JSON response body, if available.</param>
    public SmriteaException(string message, int? statusCode = null, string? code = null, bool retryable = false, object? body = null)
        : base(message)
    {
        this.HTTPStatus = statusCode;
        this.Code = code ?? "INTERNAL_ERROR";
        this.Retryable = retryable;
        this.Body = body;
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="statusCode">The HTTP status code.</param>
    /// <param name="innerException">The inner exception.</param>
    /// <param name="retryable">Whether the server marked this error as retryable.</param>
    /// <param name="body">The full parsed JSON response body, if available.</param>
    public SmriteaException(string message, int? statusCode, Exception innerException, bool retryable = false, object? body = null)
        : base(message, innerException)
    {
        this.HTTPStatus = statusCode;
        this.Code = "INTERNAL_ERROR";
        this.Retryable = retryable;
        this.Body = body;
    }

    /// <summary>Initializes a new instance of the <see cref="SmriteaException"/> class.</summary>
    public SmriteaException()
        : base()
    {
        this.Code = "INTERNAL_ERROR";
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    public SmriteaException(string? message)
        : base(message)
    {
        this.Code = "INTERNAL_ERROR";
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="innerException">The inner exception.</param>
    public SmriteaException(string? message, Exception? innerException)
        : base(message, innerException)
    {
        this.Code = "INTERNAL_ERROR";
    }

    /// <summary>Gets the HTTP status code associated with this error, or null if not applicable.</summary>
    public int? HTTPStatus { get; }

    /// <summary>Gets the machine-readable wire code from the server response, or "INTERNAL_ERROR" as default.</summary>
    public string? Code { get; }

    /// <summary>Gets a value indicating whether the server marked this error as retryable.</summary>
    public bool Retryable { get; }

    /// <summary>Gets the full parsed JSON response body associated with this error, or null if not available.</summary>
    public object? Body { get; }
}
