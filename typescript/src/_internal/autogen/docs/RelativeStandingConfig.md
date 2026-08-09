
# RelativeStandingConfig


## Properties

Name | Type
------------ | -------------
`decayFactor` | number
`decayFunction` | string
`importance` | number

## Example

```typescript
import type { RelativeStandingConfig } from ''

// TODO: Update the object below with actual values
const example = {
  "decayFactor": 0.2,
  "decayFunction": exponential,
  "importance": 0.8,
} satisfies RelativeStandingConfig

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as RelativeStandingConfig
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


