"""URL safety and SSRF prevention validation."""

import ipaddress
import socket
from urllib.parse import urlparse
from app.core.exceptions import InvalidInputException


PRIVATE_IP_NETWORKS = [
    ipaddress.ip_network("127.0.0.0/8"),
    ipaddress.ip_network("10.0.0.0/8"),
    ipaddress.ip_network("172.16.0.0/12"),
    ipaddress.ip_network("192.168.0.0/16"),
    ipaddress.ip_network("169.254.0.0/16"),
    ipaddress.ip_network("::1/128"),
    ipaddress.ip_network("fc00::/7"),
    ipaddress.ip_network("fe80::/10"),
]


def validate_public_url(url: str) -> str:
    """
    Validates that a URL is a legitimate public web address.
    Guards against SSRF, internal metadata lookups, and private networks.
    """
    try:
        parsed = urlparse(url)
    except Exception as e:
        raise InvalidInputException(f"Invalid URL structure: {str(e)}")

    if parsed.scheme.lower() not in ("http", "https"):
        raise InvalidInputException(f"Unsupported URL scheme '{parsed.scheme}'. Only HTTP and HTTPS are allowed.")

    hostname = parsed.hostname
    if not hostname:
        raise InvalidInputException("URL does not contain a valid host name.")

    # Check for localhost variations
    if hostname.lower() in ("localhost", "0.0.0.0", "metadata.google.internal"):
        raise InvalidInputException("Localhost and internal metadata endpoints are strictly forbidden.")

    # Resolve IP and verify not private network
    try:
        ip_str = socket.gethostbyname(hostname)
        ip_addr = ipaddress.ip_address(ip_str)
        for net in PRIVATE_IP_NETWORKS:
            if ip_addr in net:
                raise InvalidInputException(f"Host '{hostname}' resolves to private network IP '{ip_str}', access denied.")
    except socket.gaierror:
        # Host cannot be resolved or DNS temporarily unavailable
        pass
    except InvalidInputException:
        raise
    except Exception:
        pass

    return url
