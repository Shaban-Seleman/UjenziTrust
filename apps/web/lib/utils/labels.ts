const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

export function formatShortId(value?: string | null, fallback = "-") {
  if (!value) {
    return fallback;
  }

  if (UUID_RE.test(value)) {
    return value.slice(0, 8);
  }

  if (value.length > 16) {
    return `${value.slice(0, 8)}...${value.slice(-4)}`;
  }

  return value;
}

export function formatEntityLabel(entity: string, value?: string | null, fallback = "-") {
  if (!value) {
    return fallback;
  }

  return `${entity} ${formatShortId(value, fallback)}`;
}

export function formatUserLabel(role: string, value?: string | null, fallback = "-") {
  if (!value) {
    return fallback;
  }

  return `${role} ${formatShortId(value, fallback)}`;
}
