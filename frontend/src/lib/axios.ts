import Axios, { AxiosError } from 'axios';
import type { AxiosRequestConfig } from 'axios';

export const AXIOS_INSTANCE = Axios.create({
  baseURL: import.meta.env.BASE_URL, // use your own URL or environment variable
});

export interface ApiError {
  code: string;
  details?: Record<string, string[]>;
}


AXIOS_INSTANCE.interceptors.response.use(
  (response) => response,
  (error: AxiosError<ApiError>) => {
    if (error.response) {
      console.error("API Error: " + error.response.data.code);
    } else {
      console.error("Unknown Error")
    }
  }
);

export const customInstance = <T>(
  config: AxiosRequestConfig,
  options?: AxiosRequestConfig,
): Promise<T> => {
  const promise = AXIOS_INSTANCE({
    ...config,
    ...options,
  }).then(({ data }) => data);

  return promise;
};
