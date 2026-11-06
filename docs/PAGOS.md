# Recibir los pagos de Premium

El dinero llega a la cuenta de Mercado Pago dueña del token que configures. La app nunca ve ese
token ni las tarjetas: todo el cobro pasa por la web de Mercado Pago y lo confirma el servidor.

## Una sola vez

1. Activa el plan **Blaze** en tu proyecto de Firebase. Sin él no funcionan las Cloud Functions.
2. Entra en [tus integraciones de Mercado Pago](https://www.mercadopago.com/developers/panel/app)
   y crea una aplicación de pagos online.
3. Copia el **Access Token de producción** (empieza por `APP_USR-`).
4. Guárdalo en Firebase. El comando te lo pide sin que quede escrito en ningún archivo:

```bash
npx firebase-tools functions:secrets:set MERCADOPAGO_ACCESS_TOKEN
```

5. Publica las funciones:

```bash
npx firebase-tools deploy --only functions
```

6. En tu aplicación de Mercado Pago, añade en **Webhooks** esta dirección y activa el evento
   de pagos:

```
https://us-central1-nexuschat-d3191.cloudfunctions.net/premiumApi/webhook
```

Nunca escribas el token en el código, en `functions/index.js` ni en un chat: quien lo tenga puede
operar tu cuenta.

## Cambiar los precios

No hace falta publicar nada nuevo. En la consola de Firebase, Realtime Database:

- `config/premium/plans/monthly/amount`, `annual/amount` y `lifetime/amount`: el precio.
- `config/premium/currency`: la moneda, por ejemplo `COP`.

## Probarlo

Compra un plan desde la app y comprueba que Premium se activa solo. Si falla, mira el registro:

```bash
npx firebase-tools functions:log --only premiumApi
```
