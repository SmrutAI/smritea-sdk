// <copyright file="Memory.cs" company="PlaceholderCompany">
// Copyright (c) PlaceholderCompany. All rights reserved.
// </copyright>

namespace Smritea.Sdk;

using Newtonsoft.Json.Linq;
using Smritea.Internal.Autogen.Model;

/// <summary>
/// Public-facing Memory type. Delegates to the auto-generated <see cref="MemoryMemoryResponse"/>.
///
/// <para>In production, instances are created internally by <see cref="SmriteaClient"/> from
/// deserialized autogen responses. Use <see cref="NewBuilder"/> for test construction.</para>
/// </summary>
public sealed class Memory
{
    private readonly MemoryMemoryResponse inner;

    /// <summary>
    /// Initializes a new instance of the <see cref="Memory"/> class.
    /// Internal constructor used by SmriteaClient after deserializing the server response
    /// into the autogen type.
    /// </summary>
    internal Memory(MemoryMemoryResponse inner)
    {
        this.inner = inner;
    }

    // -- Required fields -------------------------------------------------------

    /// <summary>Gets the memory ID.</summary>
    public string? Id => this.inner.Id;

    /// <summary>Gets the app ID this memory belongs to.</summary>
    public string? AppId => this.inner.AppId;

    /// <summary>Gets the memory content text.</summary>
    public string? Content => this.inner.Content;

    /// <summary>Gets iSO-8601 creation timestamp.</summary>
    public string? CreatedAt => this.inner.CreatedAt;

    /// <summary>Gets iSO-8601 last update timestamp.</summary>
    public string? UpdatedAt => this.inner.UpdatedAt;

    // -- Optional fields -------------------------------------------------------

    /// <summary>Gets the actor ID who created this memory.</summary>
    public string? ActorId => this.inner.ActorId;

    /// <summary>Gets the type of actor (e.g. "user", "agent").</summary>
    public string? ActorType => this.inner.ActorType;

    /// <summary>Gets the display name of the actor.</summary>
    public string? ActorName => this.inner.ActorName;

    /// <summary>
    /// Gets arbitrary key-value metadata. The autogen type stores this as <c>Object?</c>
    /// (typically a <see cref="JObject"/> from Newtonsoft.Json); this property converts it to a typed dictionary.
    /// </summary>
    public Dictionary<string, object>? Metadata
    {
        get
        {
            var raw = this.inner.Metadata;
            if (raw is JObject jObj)
            {
                return jObj.ToObject<Dictionary<string, object>>();
            }

            if (raw is Dictionary<string, object> dict)
            {
                return dict;
            }

            return null;
        }
    }

    /// <summary>Gets the conversation ID this memory belongs to.</summary>
    public string? ConversationId => this.inner.ConversationId;

    /// <summary>Gets the specific message ID within the conversation.</summary>
    public string? ConversationMessageId => this.inner.ConversationMessageId;

    /// <summary>Gets iSO-8601 timestamp for when this memory becomes active.</summary>
    public string? ActiveFrom => this.inner.ActiveFrom;

    /// <summary>Gets iSO-8601 timestamp for when this memory expires.</summary>
    public string? ActiveTo => this.inner.ActiveTo;

    // -- Builder ---------------------------------------------------------------

    /// <summary>
    /// Returns a new <see cref="MemoryBuilder"/> with the required fields pre-set.
    /// Primarily useful for tests.
    /// </summary>
    /// <param name="id">The memory identifier.</param>
    /// <param name="appId">The application identifier this memory belongs to.</param>
    /// <param name="content">The memory content text.</param>
    /// <param name="createdAt">ISO-8601 creation timestamp.</param>
    /// <param name="updatedAt">ISO-8601 last-update timestamp.</param>
    /// <returns>A new <see cref="MemoryBuilder"/> with the required fields pre-set.</returns>
    public static MemoryBuilder NewBuilder(
        string? id, string? appId, string? content, string? createdAt, string? updatedAt)
    {
        return new MemoryBuilder(id, appId, content, createdAt, updatedAt);
    }

    /// <summary>Builder for constructing <see cref="Memory"/> instances with named fields.</summary>
    public sealed class MemoryBuilder
    {
        private readonly MemoryMemoryResponse @delegate = new();

        internal MemoryBuilder(
            string? id, string? appId, string? content, string? createdAt, string? updatedAt)
        {
            this.@delegate.Id = id;
            this.@delegate.AppId = appId;
            this.@delegate.Content = content;
            this.@delegate.CreatedAt = createdAt;
            this.@delegate.UpdatedAt = updatedAt;
        }

        /// <summary>Sets the actor ID.</summary>
        /// <param name="actorId">The actor identifier to set.</param>
        /// <returns>The current builder instance for method chaining.</returns>
        public MemoryBuilder WithActorId(string actorId)
        {
            this.@delegate.ActorId = actorId;
            return this;
        }

        /// <summary>Sets the actor type.</summary>
        /// <param name="actorType">The actor type to set (e.g. "user", "agent").</param>
        /// <returns>The current builder instance for method chaining.</returns>
        public MemoryBuilder WithActorType(string actorType)
        {
            this.@delegate.ActorType = actorType;
            return this;
        }

        /// <summary>Sets the actor display name.</summary>
        /// <param name="actorName">The human-readable name of the actor.</param>
        /// <returns>The current builder instance for method chaining.</returns>
        public MemoryBuilder WithActorName(string actorName)
        {
            this.@delegate.ActorName = actorName;
            return this;
        }

        /// <summary>Sets the metadata dictionary attached to the memory.</summary>
        /// <param name="metadata">Arbitrary key-value pairs to store alongside the memory.</param>
        /// <returns>The current builder instance for method chaining.</returns>
        public MemoryBuilder WithMetadata(Dictionary<string, object> metadata)
        {
            this.@delegate.Metadata = metadata;
            return this;
        }

        /// <summary>Sets the conversation ID.</summary>
        /// <param name="conversationId">The conversation identifier to associate with the memory.</param>
        /// <returns>The current builder instance for method chaining.</returns>
        public MemoryBuilder WithConversationId(string conversationId)
        {
            this.@delegate.ConversationId = conversationId;
            return this;
        }

        /// <summary>Sets the conversation message ID.</summary>
        /// <param name="conversationMessageId">The specific message ID within the conversation.</param>
        /// <returns>The current builder instance for method chaining.</returns>
        public MemoryBuilder WithConversationMessageId(string conversationMessageId)
        {
            this.@delegate.ConversationMessageId = conversationMessageId;
            return this;
        }

        /// <summary>Sets the active-from timestamp.</summary>
        /// <param name="activeFrom">ISO-8601 timestamp for when this memory becomes active.</param>
        /// <returns>The current builder instance for method chaining.</returns>
        public MemoryBuilder WithActiveFrom(string activeFrom)
        {
            this.@delegate.ActiveFrom = activeFrom;
            return this;
        }

        /// <summary>Sets the active-to timestamp.</summary>
        /// <param name="activeTo">ISO-8601 timestamp for when this memory expires.</param>
        /// <returns>The current builder instance for method chaining.</returns>
        public MemoryBuilder WithActiveTo(string activeTo)
        {
            this.@delegate.ActiveTo = activeTo;
            return this;
        }

        /// <summary>Builds an immutable <see cref="Memory"/> instance from the accumulated fields.</summary>
        /// <returns>An immutable <see cref="Memory"/> wrapping the configured response.</returns>
        public Memory Build()
        {
            return new Memory(this.@delegate);
        }
    }
}
