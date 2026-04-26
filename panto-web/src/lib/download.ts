import type { AxiosResponse } from 'axios';

function sanitizeFileName(fileName: string) {
  return fileName.replace(/^["']|["']$/g, '').trim();
}

function readFileNameFromDisposition(contentDisposition?: string | null) {
  if (!contentDisposition) {
    return null;
  }

  const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i);
  if (utf8Match?.[1]) {
    return sanitizeFileName(decodeURIComponent(utf8Match[1]));
  }

  const fileNameMatch = contentDisposition.match(/filename="?([^";]+)"?/i);
  if (fileNameMatch?.[1]) {
    return sanitizeFileName(fileNameMatch[1]);
  }

  return null;
}

export function downloadBlob(blob: Blob, fileName: string) {
  const downloadUrl = URL.createObjectURL(blob);
  const anchor = document.createElement('a');

  anchor.href = downloadUrl;
  anchor.download = fileName;
  anchor.style.display = 'none';

  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  URL.revokeObjectURL(downloadUrl);
}

export function downloadBlobResponse(
  response: AxiosResponse<Blob>,
  fallbackFileName: string,
) {
  const fileName =
    readFileNameFromDisposition(response.headers['content-disposition']) ?? fallbackFileName;
  downloadBlob(response.data, fileName);
  return fileName;
}
