
# CommondtoPersonaExtractionConfig


## Properties

Name | Type
------------ | -------------
`actorTypes` | Array&lt;string&gt;
`domains` | [Array&lt;CommondtoPersonaDomainConfig&gt;](CommondtoPersonaDomainConfig.md)
`enabled` | boolean
`maxTokens` | number
`model` | string
`temperature` | number

## Example

```typescript
import type { CommondtoPersonaExtractionConfig } from ''

// TODO: Update the object below with actual values
const example = {
  "actorTypes": null,
  "domains": null,
  "enabled": null,
  "maxTokens": null,
  "model": null,
  "temperature": null,
} satisfies CommondtoPersonaExtractionConfig

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as CommondtoPersonaExtractionConfig
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


