import axios from "axios";

export interface SessionInterface {
  name: string;
  navident: string;
  roles: string[];
}

export const getSession = async function (): Promise<SessionInterface> {
  const res = await axios.get<SessionInterface>("/nom-api/api/v1/sessioninfo");
  return res.data;
};
