import QRCode from "qrcode";
import { useEffect, useState } from "react";

export function PixQrCode({ code }: { code: string }) {
  const [src, setSrc] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
    QRCode.toDataURL(code, { margin: 1, width: 440, errorCorrectionLevel: "M", color: { dark: "#111b35", light: "#ffffff" } })
      .then((url) => active && setSrc(url))
      .catch(() => active && setSrc(null));
    return () => {
      active = false;
    };
  }, [code]);

  if (!src) return null;
  return <img src={src} alt="QR code do Pix" width={220} height={220} className="mx-auto mb-3 size-[220px] rounded-lg border-2 border-navy bg-white p-2" />;
}
