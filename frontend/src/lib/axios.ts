import Axios, { AxiosError } from 'axios';
import type { AxiosRequestConfig } from 'axios';

export const AXIOS_INSTANCE = Axios.create({
  baseURL: '/api',
  withCredentials: true,
});

export interface ApiError {
  code: string;
  details?: Record<string, unknown>;
}

export interface Failure {
  status: string;
  errors: ApiError[];
}

export const customInstance = <T>(
  config: AxiosRequestConfig,
  options?: AxiosRequestConfig,
): Promise<T> => {
  return AXIOS_INSTANCE({ ...config, ...options }).then(({ data }) => data);
};

export function firstErrorCode(error: unknown): string | undefined {
  if (error instanceof AxiosError) {
    const data = error.response?.data as Failure | undefined;
    return data?.errors?.[0]?.code;
  }
  return undefined;
}
