import axios from "axios";

export interface SessionInterface {
  name: string;
}

export const getSession = async () => {
  return (await axios.get<SessionInterface>("/sessionInfo")).data;
};
