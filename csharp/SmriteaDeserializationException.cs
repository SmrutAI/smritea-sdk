// <copyright file="SmriteaDeserializationException.cs" company="PlaceholderCompany">
// Copyright (c) PlaceholderCompany. All rights reserved.
// </copyright>

namespace Smritea.Sdk;

/// <summary>
/// Raised when the server response body cannot be deserialized into the expected type.
/// This indicates either a malformed response or an API contract mismatch.
/// </summary>
public class SmriteaDeserializationException : SmriteaException
{
    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaDeserializationException"/> class.
    /// </summary>
    /// <param name="message">The error message describing the deserialization failure.</param>
    public SmriteaDeserializationException(string message)
        : base(message)
    {
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaDeserializationException"/> class.
    /// </summary>
    /// <param name="message">The error message describing the deserialization failure.</param>
    /// <param name="innerException">The inner exception that caused the failure.</param>
    public SmriteaDeserializationException(string message, Exception innerException)
        : base(message, null, innerException)
    {
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaDeserializationException"/> class.
    /// </summary>
    /// <param name="message">The error message describing the deserialization failure.</param>
    /// <param name="statusCode">The HTTP status code, if available.</param>
    public SmriteaDeserializationException(string message, int? statusCode = null)
        : base(message, statusCode)
    {
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaDeserializationException"/> class.
    /// </summary>
    /// <param name="message">The error message describing the deserialization failure.</param>
    /// <param name="statusCode">The HTTP status code.</param>
    /// <param name="innerException">The inner exception that caused the failure.</param>
    public SmriteaDeserializationException(string message, int? statusCode, Exception innerException)
        : base(message, statusCode, innerException)
    {
    }

    /// <summary>Initializes a new instance of the <see cref="SmriteaDeserializationException"/> class.</summary>
    public SmriteaDeserializationException()
        : base()
    {
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaDeserializationException"/> class.
    /// </summary>
    /// <param name="message">The error message describing the deserialization failure.</param>
    /// <param name="statusCode">The HTTP status code, if available.</param>
    /// <param name="code">The machine-readable wire code from the server response.</param>
    public SmriteaDeserializationException(string message, int? statusCode = null, string? code = null)
        : base(message, statusCode, code)
    {
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaDeserializationException"/> class.
    /// </summary>
    /// <param name="message">The error message describing the deserialization failure.</param>
    /// <param name="statusCode">The HTTP status code, if available.</param>
    /// <param name="code">The machine-readable wire code from the server response.</param>
    /// <param name="body">The full parsed JSON response body, if available.</param>
    public SmriteaDeserializationException(string message, int? statusCode = null, string? code = null, object? body = null)
        : base(message, statusCode, code, false, body)
    {
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaDeserializationException"/> class.
    /// </summary>
    /// <param name="message">The error message describing the deserialization failure.</param>
    /// <param name="statusCode">The HTTP status code, if available.</param>
    /// <param name="innerException">The inner exception that caused the failure.</param>
    /// <param name="body">The full parsed JSON response body, if available.</param>
    public SmriteaDeserializationException(string message, int? statusCode, Exception innerException, object? body = null)
        : base(message, statusCode, innerException, false, body)
    {
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaDeserializationException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="statusCode">The HTTP status code, if available.</param>
    /// <param name="code">The machine-readable wire code from the server response.</param>
    /// <param name="retryable">Whether the server marked this error as retryable.</param>
    /// <param name="body">The full parsed JSON response body, if available.</param>
    public SmriteaDeserializationException(string message, int? statusCode = null, string? code = null, bool retryable = false, object? body = null)
        : base(message, statusCode, code, retryable, body)
    {
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaDeserializationException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="statusCode">The HTTP status code, if available.</param>
    /// <param name="innerException">The inner exception.</param>
    /// <param name="retryable">Whether the server marked this error as retryable.</param>
    /// <param name="body">The full parsed JSON response body, if available.</param>
    public SmriteaDeserializationException(string message, int? statusCode, Exception innerException, bool retryable = false, object? body = null)
        : base(message, statusCode, innerException, retryable, body)
    {
    }
}
