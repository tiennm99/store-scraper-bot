import { pino } from 'pino';

export function createLogger(env) {
  const isDev = env !== 'PRODUCTION';
  return pino(
    isDev
      ? {
          level: 'debug',
          transport: {
            target: 'pino-pretty',
            options: { colorize: true, translateTime: 'SYS:HH:MM:ss', ignore: 'pid,hostname' },
          },
        }
      : { level: 'info' },
  );
}
