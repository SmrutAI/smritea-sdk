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
    /// <param name="errorCode">The machine-readable error code from the server response.</param>
    public SmriteaDeserializationException(string message, int? statusCode = null, string? errorCode = null)
        : base(message, statusCode, errorCode)
    {
    }
}
