# SDKMemoryApi

All URIs are relative to *http://api.smritea.ai/api/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**createMemory**](SDKMemoryApi.md#creatememory) | **POST** /api/v1/sdk/memories | Create memory (SDK) |
| [**deleteMemory**](SDKMemoryApi.md#deletememory) | **DELETE** /api/v1/sdk/memories/{memory_id} | Delete memory (SDK) |
| [**getMemory**](SDKMemoryApi.md#getmemory) | **GET** /api/v1/sdk/memories/{memory_id} | Get memory by ID (SDK) |
| [**searchMemories**](SDKMemoryApi.md#searchmemories) | **POST** /api/v1/sdk/memories/search | Search memories (SDK) |



## createMemory

> MemoryCreateMemoryResponse createMemory(request)

Create memory (SDK)

Create a new memory with quota and rate limit enforcement

### Example

```ts
import {
  Configuration,
  SDKMemoryApi,
} from '';
import type { CreateMemoryRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // To configure API key authorization: ApiKeyAuth
    apiKey: "YOUR API KEY",
  });
  const api = new SDKMemoryApi(config);

  const body = {
    // MemoryCreateMemoryRequest | Memory creation details
    request: ...,
  } satisfies CreateMemoryRequest;

  try {
    const data = await api.createMemory(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **request** | [MemoryCreateMemoryRequest](MemoryCreateMemoryRequest.md) | Memory creation details | |

### Return type

[**MemoryCreateMemoryResponse**](MemoryCreateMemoryResponse.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | Memory created successfully |  -  |
| **400** | Invalid request |  -  |
| **401** | Unauthorized - invalid or missing API key |  -  |
| **402** | Quota exhausted - upgrade your plan |  -  |
| **429** | Rate limit exceeded - please retry later |  -  |
| **500** | Internal server error |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## deleteMemory

> deleteMemory(memoryId)

Delete memory (SDK)

Delete a memory by ID with rate limit enforcement

### Example

```ts
import {
  Configuration,
  SDKMemoryApi,
} from '';
import type { DeleteMemoryRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // To configure API key authorization: ApiKeyAuth
    apiKey: "YOUR API KEY",
  });
  const api = new SDKMemoryApi(config);

  const body = {
    // string | Memory ID
    memoryId: memoryId_example,
  } satisfies DeleteMemoryRequest;

  try {
    const data = await api.deleteMemory(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **memoryId** | `string` | Memory ID | [Defaults to `undefined`] |

### Return type

`void` (Empty response body)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Memory deleted successfully |  -  |
| **400** | Invalid request |  -  |
| **401** | Unauthorized - invalid or missing API key |  -  |
| **404** | Memory not found |  -  |
| **429** | Rate limit exceeded - please retry later |  -  |
| **500** | Internal server error |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getMemory

> MemoryMemoryResponse getMemory(memoryId)

Get memory by ID (SDK)

Get a single memory by ID with rate limit enforcement

### Example

```ts
import {
  Configuration,
  SDKMemoryApi,
} from '';
import type { GetMemoryRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // To configure API key authorization: ApiKeyAuth
    apiKey: "YOUR API KEY",
  });
  const api = new SDKMemoryApi(config);

  const body = {
    // string | Memory ID
    memoryId: memoryId_example,
  } satisfies GetMemoryRequest;

  try {
    const data = await api.getMemory(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **memoryId** | `string` | Memory ID | [Defaults to `undefined`] |

### Return type

[**MemoryMemoryResponse**](MemoryMemoryResponse.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Memory details |  -  |
| **400** | Invalid request |  -  |
| **401** | Unauthorized - invalid or missing API key |  -  |
| **404** | Memory not found |  -  |
| **429** | Rate limit exceeded - please retry later |  -  |
| **500** | Internal server error |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## searchMemories

> MemorySearchMemoriesResponse searchMemories(request)

Search memories (SDK)

Search memories with quota and rate limit enforcement

### Example

```ts
import {
  Configuration,
  SDKMemoryApi,
} from '';
import type { SearchMemoriesRequest } from '';

async function example() {
  console.log("🚀 Testing  SDK...");
  const config = new Configuration({ 
    // To configure API key authorization: ApiKeyAuth
    apiKey: "YOUR API KEY",
  });
  const api = new SDKMemoryApi(config);

  const body = {
    // MemorySearchMemoryRequest | Search request details
    request: ...,
  } satisfies SearchMemoriesRequest;

  try {
    const data = await api.searchMemories(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **request** | [MemorySearchMemoryRequest](MemorySearchMemoryRequest.md) | Search request details | |

### Return type

[**MemorySearchMemoriesResponse**](MemorySearchMemoriesResponse.md)

### Authorization

[ApiKeyAuth](../README.md#ApiKeyAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Search results |  -  |
| **400** | Invalid request |  -  |
| **401** | Unauthorized - invalid or missing API key |  -  |
| **402** | Quota exhausted - upgrade your plan |  -  |
| **429** | Rate limit exceeded - please retry later |  -  |
| **500** | Internal server error |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

