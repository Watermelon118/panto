import axios from 'axios';

export function extractErrorMessage(error: unknown): string {
  if (axios.isAxiosError<{ message?: string }>(error)) {
    return error.response?.data?.message ?? 'Something went wrong.';
  }
  return 'Something went wrong.';
}
