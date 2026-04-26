import axios from 'axios';

export const apiClient = axios.create({
  baseURL: 'http://192.168.88.200:8080/api/v1',
  withCredentials: true,
  timeout: 10000,
});
