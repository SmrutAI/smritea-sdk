// <copyright file="RelativeStandingConfig.WireFormat.cs" company="Bytonomics LLP">
// Copyright (c) Bytonomics LLP. All rights reserved.
// </copyright>

namespace Smritea.Internal.Autogen.Model;

using Newtonsoft.Json;

/// <summary>
/// Hand-written half of the generated <see cref="RelativeStandingConfig"/>. It lives outside
/// <c>_internal/autogen</c> so a regeneration never overwrites it.
/// The generator declares <c>Importance</c> and <c>DecayFactor</c> as non-nullable <c>decimal</c> with
/// <c>EmitDefaultValue = false</c>, so a plain serialization drops an explicit <c>0</c> and the server then
/// applies its defaults (importance 1.0, decay_factor 0.2) instead of the value the caller chose.
/// <see cref="RelativeStandingConfigConverter"/> writes a field when it is non-zero or was set explicitly.
/// If the generator ever emits these fields as nullable, the flags stay correct and this file can be deleted.
/// </summary>
// TODO: Remove this partial class, RelativeStandingConfigConverter and the flag assignments in SmriteaClient.cs
// once the generator emits nullable value types (decimal?) for optional fields, for example through an
// x-nullable spec marker or a template override. The change must not alter the public SDK contract:
// RelativeStanding and the With* builders keep the same signatures for SDK users.
[JsonConverter(typeof(RelativeStandingConfigConverter))]
public partial class RelativeStandingConfig
{
    /// <summary>Gets or sets a value indicating whether <see cref="Importance"/> was chosen by the caller, so a <c>0</c> must still be sent.</summary>
    internal bool ImportanceIsSet { get; set; }

    /// <summary>Gets or sets a value indicating whether <see cref="DecayFactor"/> was chosen by the caller, so a <c>0</c> must still be sent.</summary>
    internal bool DecayFactorIsSet { get; set; }
}
