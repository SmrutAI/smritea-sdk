// <copyright file="SmriteaException.cs" company="PlaceholderCompany">
// Copyright (c) PlaceholderCompany. All rights reserved.
// </copyright>

namespace Smritea.Sdk;

/// <summary>Base exception for all smritea SDK errors.</summary>
public class SmriteaException : Exception
{
    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="statusCode">The HTTP status code, if available.</param>
    public SmriteaException(string message, int? statusCode = null)
        : base(message)
    {
        this.StatusCode = statusCode;
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="statusCode">The HTTP status code.</param>
    /// <param name="innerException">The inner exception.</param>
    public SmriteaException(string message, int? statusCode, Exception innerException)
        : base(message, innerException)
    {
        this.StatusCode = statusCode;
    }

    /// <summary>Initializes a new instance of the <see cref="SmriteaException"/> class.</summary>
    public SmriteaException()
        : base()
    {
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    public SmriteaException(string? message)
        : base(message)
    {
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaException"/> class.
    /// </summary>
    /// <param name="message">The error message.</param>
    /// <param name="innerException">The inner exception.</param>
    public SmriteaException(string? message, Exception? innerException)
        : base(message, innerException)
    {
    }

    /// <summary>Gets the HTTP status code associated with this error, or null if not applicable.</summary>
    public int? StatusCode { get; }
}
