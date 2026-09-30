
# PersonaExtractionConfig


## Properties

Name | Type
------------ | -------------
`actorTypes` | Array&lt;string&gt;
`domains` | [Array&lt;PersonaDomainConfig&gt;](PersonaDomainConfig.md)
`enabled` | boolean
`model` | string

## Example

```typescript
import type { PersonaExtractionConfig } from ''

// TODO: Update the object below with actual values
const example = {
  "actorTypes": null,
  "domains": null,
  "enabled": null,
  "model": null,
} satisfies PersonaExtractionConfig

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as PersonaExtractionConfig
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


