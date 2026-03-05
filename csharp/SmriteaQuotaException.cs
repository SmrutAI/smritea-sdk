// <copyright file="SmriteaQuotaException.cs" company="PlaceholderCompany">
// Copyright (c) PlaceholderCompany. All rights reserved.
// </copyright>

namespace Smritea.Sdk;

/// <summary>Raised on HTTP 402 — quota or payment required.</summary>
public class SmriteaQuotaException : SmriteaException
{
    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaQuotaException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="statusCode">The HTTP status code.</param>
    public SmriteaQuotaException(string message, int statusCode)
        : base(message, statusCode)
    {
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaQuotaException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="statusCode">The HTTP status code, if available.</param>
    public SmriteaQuotaException(string message, int? statusCode = null)
        : base(message, statusCode)
    {
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaQuotaException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="statusCode">The HTTP status code.</param>
    /// <param name="innerException">The inner exception.</param>
    public SmriteaQuotaException(string message, int? statusCode, Exception innerException)
        : base(message, statusCode, innerException)
    {
    }

    /// <summary>Initializes a new instance of the <see cref="SmriteaQuotaException"/> class.</summary>
    public SmriteaQuotaException()
        : base()
    {
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaQuotaException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    public SmriteaQuotaException(string? message)
        : base(message)
    {
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaQuotaException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="innerException">The inner exception.</param>
    public SmriteaQuotaException(string? message, Exception? innerException)
        : base(message, innerException)
    {
    }
}
