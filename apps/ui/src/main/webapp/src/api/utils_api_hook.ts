export type LookupState<T> =
  | {
      lookupState: "WAITING";
    }
  | {
      lookupState: "PENDING";
    }
  | {
      lookupState: "ERROR";
      error: Error;
    }
  | {
      lookupState: "RESOLVED";
      result: T;
    };

export const lookups = {
  createError: function <T>(msg: string): LookupState<T> {
    return { lookupState: "ERROR", error: new Error(msg) };
  },

  createResolved: function <T>(result: T): LookupState<T> {
    return { lookupState: "RESOLVED", result: result };
  },

  createWaiting: function createLookupWaiting<T>(): LookupState<T> {
    return { lookupState: "WAITING" };
  },

  createPending: function <T>(): LookupState<T> {
    return { lookupState: "PENDING" };
  },
};
