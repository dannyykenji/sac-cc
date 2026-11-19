// Polyfill para libs CJS antigas (sockjs-client referencia `global`, inexistente no browser).
// Módulo separado: o Vite hoista imports de main.tsx, então o polyfill precisa
// ser o PRIMEIRO import do grafo para executar antes do sockjs-client.
export {}
;(window as unknown as { global: unknown }).global = window
