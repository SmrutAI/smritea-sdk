// <copyright file="SmriteaAuthException.cs" company="PlaceholderCompany">
// Copyright (c) PlaceholderCompany. All rights reserved.
// </copyright>

namespace Smritea.Sdk;

/// <summary>Raised on HTTP 401 — invalid or missing API key.</summary>
public class SmriteaAuthException : SmriteaException
{
    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaAuthException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="statusCode">The HTTP status code.</param>
    public SmriteaAuthException(string message, int statusCode)
        : base(message, statusCode)
    {
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaAuthException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="statusCode">The HTTP status code, if available.</param>
    public SmriteaAuthException(string message, int? statusCode = null)
        : base(message, statusCode)
    {
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaAuthException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="statusCode">The HTTP status code.</param>
    /// <param name="innerException">The inner exception.</param>
    public SmriteaAuthException(string message, int? statusCode, Exception innerException)
        : base(message, statusCode, innerException)
    {
    }

    /// <summary>Initializes a new instance of the <see cref="SmriteaAuthException"/> class.</summary>
    public SmriteaAuthException()
        : base()
    {
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaAuthException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    public SmriteaAuthException(string? message)
        : base(message)
    {
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaAuthException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="innerException">The inner exception.</param>
    public SmriteaAuthException(string? message, Exception? innerException)
        : base(message, innerException)
    {
    }
}
