
export const sortItems = (a: string, b: string) => {
    if (a.localeCompare(b, 'no') < 0) {
      return -1
    } else if (a.localeCompare(b, 'no') > 0) {
      return 1
    }
    return 0
}