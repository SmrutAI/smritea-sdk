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
    /// <param name="errorCode">The machine-readable error code from the server response.</param>
    public SmriteaNotFoundException(string message, int statusCode, string? errorCode = null)
        : base(message, statusCode, errorCode)
    {
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaNotFoundException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="statusCode">The HTTP status code, if available.</param>
    /// <param name="errorCode">The machine-readable error code from the server response.</param>
    public SmriteaNotFoundException(string message, int? statusCode = null, string? errorCode = null)
        : base(message, statusCode, errorCode)
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
}
