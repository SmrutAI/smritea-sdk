// <copyright file="RelativeStandingConfigConverter.cs" company="Bytonomics LLP">
// Copyright (c) Bytonomics LLP. All rights reserved.
// </copyright>

namespace Smritea.Internal.Autogen.Model;

using Newtonsoft.Json;

/// <summary>
/// Writes a <see cref="RelativeStandingConfig"/> as the server expects it: <c>decay_factor</c> and
/// <c>importance</c> are sent when they are non-zero or were set explicitly, so an explicit <c>0</c>
/// ("no decay", "lowest importance") reaches the server instead of being replaced by its defaults.
/// Reading is not handled here, so response bodies use the default deserialization.
/// </summary>
// TODO: Remove this converter, the RelativeStandingConfig partial class and the flag assignments in
// SmriteaClient.cs once the generator emits nullable value types (decimal?) for optional fields, for example
// through an x-nullable spec marker or a template override. The change must not alter the public SDK
// contract: RelativeStanding and the With* builders keep the same signatures for SDK users.
internal sealed class RelativeStandingConfigConverter : JsonConverter<RelativeStandingConfig>
{
    /// <inheritdoc/>
    public override bool CanRead => false;

    /// <inheritdoc/>
    public override void WriteJson(JsonWriter writer, RelativeStandingConfig? value, JsonSerializer serializer)
    {
        if (value is null)
        {
            writer.WriteNull();
            return;
        }

        writer.WriteStartObject();

        if (value.DecayFactorIsSet || value.DecayFactor != 0)
        {
            writer.WritePropertyName("decay_factor");
            writer.WriteValue(value.DecayFactor);
        }

        if (value.DecayFunction is not null)
        {
            writer.WritePropertyName("decay_function");
            serializer.Serialize(writer, value.DecayFunction);
        }

        if (value.ImportanceIsSet || value.Importance != 0)
        {
            writer.WritePropertyName("importance");
            writer.WriteValue(value.Importance);
        }

        writer.WriteEndObject();
    }

    /// <inheritdoc/>
    public override RelativeStandingConfig ReadJson(
        JsonReader reader,
        Type objectType,
        RelativeStandingConfig? existingValue,
        bool hasExistingValue,
        JsonSerializer serializer)
    {
        throw new NotSupportedException("RelativeStandingConfigConverter only writes JSON.");
    }
}
