// <copyright file="SmriteaValidationException.cs" company="PlaceholderCompany">
// Copyright (c) PlaceholderCompany. All rights reserved.
// </copyright>

namespace Smritea.Sdk;

/// <summary>Raised on HTTP 400 — request validation failed.</summary>
public class SmriteaValidationException : SmriteaException
{
    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaValidationException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="statusCode">The HTTP status code.</param>
    /// <param name="errorCode">The machine-readable error code from the server response.</param>
    public SmriteaValidationException(string message, int statusCode, string? errorCode = null)
        : base(message, statusCode, errorCode)
    {
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaValidationException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="statusCode">The HTTP status code, if available.</param>
    /// <param name="errorCode">The machine-readable error code from the server response.</param>
    public SmriteaValidationException(string message, int? statusCode = null, string? errorCode = null)
        : base(message, statusCode, errorCode)
    {
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaValidationException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="statusCode">The HTTP status code.</param>
    /// <param name="innerException">The inner exception.</param>
    public SmriteaValidationException(string message, int? statusCode, Exception innerException)
        : base(message, statusCode, innerException)
    {
    }

    /// <summary>Initializes a new instance of the <see cref="SmriteaValidationException"/> class.</summary>
    public SmriteaValidationException()
        : base()
    {
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaValidationException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    public SmriteaValidationException(string? message)
        : base(message)
    {
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaValidationException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="innerException">The inner exception.</param>
    public SmriteaValidationException(string? message, Exception? innerException)
        : base(message, innerException)
    {
    }
}
