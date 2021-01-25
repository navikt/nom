export type LookupState =
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
      result: any;
    };

export const lookups = {
  createError: function (msg: string): LookupState {
    return { lookupState: "ERROR", error: new Error(msg) };
  },

  createResolved: function (
    result: any,
    subNav?: (obj: any) => any
  ): LookupState {
    const tmp = subNav ? subNav(result) : result;
    return { lookupState: "RESOLVED", result: tmp };
  },

  createWaiting: function createLookupWaiting(): LookupState {
    return { lookupState: "WAITING" };
  },

  createPending: function (): LookupState {
    return { lookupState: "PENDING" };
  },
};
